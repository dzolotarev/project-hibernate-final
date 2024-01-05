package ru.dzolotarev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ru.dzolotarev.dao.CityDAO;
import ru.dzolotarev.dao.CountryDAO;
import ru.dzolotarev.entity.City;
import ru.dzolotarev.entity.Country;
import ru.dzolotarev.entity.CountryLanguage;
import ru.dzolotarev.redis.CityCountry;
import ru.dzolotarev.redis.Language;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;
import static ru.dzolotarev.utils.PrepareUtil.prepareRedisClient;
import static ru.dzolotarev.utils.PrepareUtil.prepareRelationalDb;

public class App {

    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper mapper;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public App() {

        sessionFactory = prepareRelationalDb();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);
        redisClient = prepareRedisClient();
        mapper = new ObjectMapper();
    }

    public static void main(String[] args) {

        App app = new App();
        List<City> allCities = app.fetchData(app);
        List<CityCountry> preparedData = app.transformData(allCities);
        app.pushToRedis(preparedData);

        //закроем текущую сессию, чтоб точно делать запрос к БД, а не вытянуть данные из кэша
        app.sessionFactory.getCurrentSession().close();

        //выбираем случайных 10 id городов
        //так как мы не делали обработку невалидных ситуаций, используй существующие в БД id
        List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

        long startRedis = System.currentTimeMillis();
        app.testRedisData(ids);
        long stopRedis = System.currentTimeMillis();

        long startMysql = System.currentTimeMillis();
        app.testMysqlData(ids);
        long stopMysql = System.currentTimeMillis();

        System.out.printf("%5s:\t%5d ms\n", "Redis", (stopRedis - startRedis));
        System.out.printf("%5s:\t%5d ms\n", "MySQL", (stopMysql - startMysql));

        app.shutdown();
    }

    private static Set<Language> getLanguages(Country country) {

        Set<Language> languages = new HashSet<>();
        Set<CountryLanguage> countryLanguages = country.getLanguages();
        for (CountryLanguage countryLanguage : countryLanguages) {
            Language language = new Language();
            language.setLanguage(countryLanguage.getLanguage());
            language.setIsOfficial(countryLanguage.getIsOfficial());
            language.setPercentage(countryLanguage.getPercentage());
            languages.add(language);
        }
        return languages;
    }

    private List<City> fetchData(App app) {

        List<City> allCities = new ArrayList<>();
        Session session = sessionFactory.getCurrentSession();
        try (session) {
            session.beginTransaction();
            List<Country> countries = app.countryDAO.getAll();
            int totalCount = app.cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(app.cityDAO.getItems(i, step));
            }
            session.getTransaction().commit();
        } catch (RuntimeException e) {
            session.getTransaction().rollback();
            e.printStackTrace();
        }
        return allCities;
    }

    private List<CityCountry> transformData(List<City> cities) {

        List<CityCountry> cityCountryList = new ArrayList<>();

        for (City city : cities) {
            CityCountry cityCountry = new CityCountry();

            cityCountry.setId(city.getId());
            cityCountry.setName(city.getName());
            cityCountry.setPopulation(city.getPopulation());
            cityCountry.setDistrict(city.getDistrict());

            Country country = city.getCountry();

            cityCountry.setAlternativeCountryCode(country.getAlternativeCode());
            cityCountry.setContinent(country.getContinent());
            cityCountry.setCountryCode(country.getCode());
            cityCountry.setCountryName(country.getName());
            cityCountry.setCountryPopulation(country.getPopulation());
            cityCountry.setCountryRegion(country.getRegion());
            cityCountry.setCountrySurfaceArea(country.getSurfaceArea());

            Set<Language> languages = getLanguages(country);

            cityCountry.setLanguages(languages);
            cityCountryList.add(cityCountry);
        }
        return cityCountryList;
    }

    private void pushToRedis(List<CityCountry> data) {

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    sync.set(String.valueOf(cityCountry.getId()), mapper.writeValueAsString(cityCountry));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testRedisData(List<Integer> ids) {

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    mapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {

        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
            }
            session.getTransaction().commit();
        }
    }

    private void shutdown() {

        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.shutdown();
        }
    }
}
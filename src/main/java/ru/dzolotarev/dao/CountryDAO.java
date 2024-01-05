package ru.dzolotarev.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import ru.dzolotarev.entity.Country;

import java.util.List;

public class CountryDAO {

    public static final String GET_ALL_HQL = "select c from Country c join fetch c.languages"; // join fetch - оптимизируем запрос языков

    private final SessionFactory sessionFactory;

    public CountryDAO(SessionFactory sessionFactory) {

        this.sessionFactory = sessionFactory;
    }

    public List<Country> getAll() {

        Session session = sessionFactory.getCurrentSession();
        Query<Country> queue = session.createQuery(GET_ALL_HQL, Country.class);
        return queue.getResultList();
    }
}

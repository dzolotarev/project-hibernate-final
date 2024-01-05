package ru.dzolotarev.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import ru.dzolotarev.entity.City;

import java.util.List;

public class CityDAO {

    public static final String GET_ITEMS_HQL = "select c from City c";
    public static final String GET_TOTAL_HQL = "select count(c) from City c";
    public static final String GET_BY_ID_HQL = "select c from City c join fetch c.country where c.id = :ID";


    private final SessionFactory sessionFactory;

    public CityDAO(SessionFactory sessionFactory) {

        this.sessionFactory = sessionFactory;
    }

    public List<City> getItems(int offset, int limit) {

        Session session = sessionFactory.getCurrentSession();
        Query<City> query = session.createQuery(GET_ITEMS_HQL, City.class);
        query.setMaxResults(limit);
        query.setFirstResult(offset);
        return query.getResultList();
    }

    public int getTotalCount() {

        Session session = sessionFactory.getCurrentSession();
        Query<Long> query = session.createQuery(GET_TOTAL_HQL, Long.class);
        return Math.toIntExact(query.getSingleResult());
    }

    public City getById(Integer id) {

        Session session = sessionFactory.getCurrentSession();
        Query<City> query = session.createQuery(GET_BY_ID_HQL, City.class);
        query.setParameter("ID", id);
        return query.getSingleResult();
    }

}

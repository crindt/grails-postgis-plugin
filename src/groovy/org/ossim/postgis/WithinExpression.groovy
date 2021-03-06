package org.ossim.postgis
/*
 * org.ossim.postgis.WithinExpression.java
 * 
 * PostGIS extension for PostgreSQL JDBC driver - EJB3 Tutorial
 * 
 * (C) 2006  Norman Barker <norman.barker@gmail.com>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA or visit the web at
 * http://www.gnu.org.
 * 
 * $Id: org.ossim.postgis.PostGISDialect.java 2531 2007-04-27 10:42:17Z mschaber $
 */
//package org.postgis.hibernate;


import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.TypedValue;

/**
 * @author nbarker
 *
 */
public class WithinExpression implements Criterion
{
  private static final long serialVersionUID = 1L;
  private String propertyName;
  private Geometry geom;

  public WithinExpression(String propertyName, Geometry geom)
  {
    this.propertyName = propertyName;
    this.geom = geom;
  }

  public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException
  {
    return [new TypedValue(Hibernate.custom(GeometryType.class), geom, EntityMode.POJO)] as  TypedValue[];
  }

  public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException
  {
    Dialect dialect = criteriaQuery.getFactory().getDialect();
    String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);

    if ( columns.length != 1 )
      throw new HibernateException("\"within\" may only be used with single-column properties");
    if ( dialect instanceof PostGISDialect )
    {
      StandardSQLFunction function = (StandardSQLFunction) dialect.getFunctions().get(PostGISDialect.NAMESPACE + "within");
      List args = new ArrayList();
      args.add(columns[0]);
      args.add("?");

      return function.render(args, criteriaQuery.getFactory());
    }
    else
    {
      throw new HibernateException("\"within\" may only be used with a spatial hibernate dialect");
    }
  }

  public String toString()
  {
    return propertyName + " within " + geom;
  }

}

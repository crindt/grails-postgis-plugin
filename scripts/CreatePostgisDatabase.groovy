//import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment: "env")

grailsHome = Ant.project.properties."environment.GRAILS_HOME"


target('default': "The description of the script goes here!") {
  doStuff()
}

target(doStuff: "The implementation task") {
  def config = new ConfigSlurper(grailsEnv).parse(new File("${basedir}/grails-app/conf/DataSource.groovy").toURL())

  def databaseName = config.dataSource.url.split(":")[-1]

  def pgHome = Ant.antProject.properties."env.PG_HOME"
  def postgisHome = Ant.antProject.properties."env.POSTGIS_HOME"

  if ( !pgHome )
  {
    System.err.println("PG_HOME environment not set")
    System.exit(-1)
  }

  if ( !postgisHome )
  {
    System.err.println("POSTGIS_HOME environment not set")
    System.exit(-1)
  }

  Ant.exec(executable: "${pgHome}/bin/createdb")
      {
        arg(value: "-U")
        arg(value: "${config.dataSource.username}")
        arg(value: "${databaseName}")
      }

  Ant.exec(executable: "${pgHome}/bin/createlang")
      {
        arg(value: "-U")
        arg(value: "${config.dataSource.username}")
        arg(value: "plpgsql")
        arg(value: "${databaseName}")
      }


  File postgisSqlFile = findPostgisSqlFile(postgisHome)
  File spatialRefSysFile = "${postgisHome}/spatial_ref_sys.sql" as File

  if ( postgisSqlFile?.exists() && spatialRefSysFile.exists() )
  {
    Ant.exec(executable: "${pgHome}/bin/psql")
        {
          arg(value: "-U")
          arg(value: "${config.dataSource.username}")
          arg(value: "-d")
          arg(value: "${databaseName}")
          arg(value: "-f")
          arg(value: postgisSqlFile.absolutePath)
        }

    Ant.exec(executable: "${pgHome}/bin/psql")
        {
          arg(value: "-U")
          arg(value: "${config.dataSource.username}")
          arg(value: "-d")
          arg(value: "${databaseName}")
          arg(value: "-f")
          arg(value: spatialRefSysFile.absolutePath)
        }
  }
  else
  {
    System.err.println("Cannot find SQL files necessary for PostGIS installation.")
    System.exit(-1)
  }
}

File findPostgisSqlFile(def postgisHome)
{
  def filenames = [
      "postgis-64.sql", // 1.4.x 64bit
      "postgis.sql", // 1.4.x 32bit
      "lwpostgis-64.sql", // 1.3.x 64bit
      "lwpostgis.sql" // 1.3.x 32bit
  ]

  File postgisSqlFile

  for ( filename in filenames )
  {
    File tempFile = new File(postgisHome, filename)

    if ( tempFile.exists() )
    {
      postgisSqlFile = tempFile
      break
    }
  }

  return postgisSqlFile
}
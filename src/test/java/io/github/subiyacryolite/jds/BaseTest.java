package io.github.subiyacryolite.jds;

import io.github.subiyacryolite.jds.classes.SimpleAddress;
import io.github.subiyacryolite.jds.classes.SimpleAddressBook;
import io.github.subiyacryolite.jds.enums.JdsImplementation;
import org.junit.Test;
import org.sqlite.SQLiteConfig;

import java.io.File;

/**
 * Created by ifung on 08/04/2017.
 */
public class BaseTest {

    protected JdsDatabase jdsDatabase;

    public BaseTest() {
        initialiseJdsClasses();
    }

    @Test
    public void initialiseJdsClasses() {
        JdsEntityClasses.map(SimpleAddress.class);
        JdsEntityClasses.map(SimpleAddressBook.class);
    }


    @Test
    public void initialiseSqlLiteBackend() {
        String url = "jdbc:sqlite:" + getDatabaseFile();
        jdsDatabase = JdsDatabase.getImplementation(JdsImplementation.SQLITE);
        SQLiteConfig sqLiteConfig = new SQLiteConfig();
        sqLiteConfig.enforceForeignKeys(true); //You must enable foreign keys in SQLite
        jdsDatabase.setConnectionProperties(url, sqLiteConfig.toProperties());
        jdsDatabase.init();
    }

    @Test
    public void initialisePostgeSqlBackend() {
        jdsDatabase = JdsDatabase.getImplementation(JdsImplementation.POSTGRES);
        jdsDatabase.setConnectionProperties("org.postgresql.Driver", "jdbc:postgresql://127.0.0.1:5432/jds", "postgres", "");
        jdsDatabase.init();
    }

    @Test
    public void initialiseTSqlBackend() {
        jdsDatabase = JdsDatabase.getImplementation(JdsImplementation.TSQL);
        jdsDatabase.setConnectionProperties("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://DESKTOP-64C7FRP\\JDSINSTANCE;databaseName=jds", "sa", "p@nkP#55W0rd");
        jdsDatabase.init();
    }

    @Test
    public void initialiseMysqlBackend() {
        jdsDatabase = JdsDatabase.getImplementation(JdsImplementation.MYSQL);
        jdsDatabase.setConnectionProperties("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/jds?autoReconnect=true&useSSL=false", "root", "");
        jdsDatabase.init();
    }

    public String getDatabaseFile() {
        File path = new File(System.getProperty("user.home") + File.separator + ".jdstest" + File.separator + "jds.db");
        if (!path.exists()) {
            File directory = path.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
        String absolutePath = path.getAbsolutePath();
        System.err.printf("You're test database is available here [%s]\n", absolutePath);
        return absolutePath;
    }
}

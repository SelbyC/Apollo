package net.jitse.apollo.mysql;

/*
 * A server overview system for Minecraft networks.
 * Copyright (C) 2017  Jitse Boonstra
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.jitse.apollo.bungeecord.ApolloBungeeCord;
import net.jitse.apollo.spigot.ApolloSpigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * @auhor Jitse B.
 * @since 10.24.2017
 */
public class MySQL {

    private ApolloSpigot spigot;
    private ApolloBungeeCord bungeecord;
    private HikariDataSource hikariDataSource;

    public MySQL(ApolloBungeeCord bungeecord) {
        this.bungeecord = bungeecord;
    }

    public MySQL(ApolloSpigot spigot) {
        this.spigot = spigot;
    }

    /**
     * Create a connection pool.
     *
     * @param host     The hostname, usually an ip.
     * @param port     The port of the database, default: 3306.
     * @param username The username of the database.
     * @param password The password to the database.
     * @param database The database name.
     * @return
     */
    public void connect(String host, Integer port, String username, String password, String database, boolean ssl) throws Exception {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ':' + (port == null ? 3306 : port) + '/' + database + "?useSSL=" + ssl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariDataSource = new HikariDataSource(hikariConfig);

            if (spigot != null) {
                spigot.getLogger().log(Level.INFO, "Connected to the database with username: \"" + username + "\".");
            } else {
                bungeecord.getLogger().log(Level.INFO, "Connected to the database with username: \"" + username + "\".");
            }
        } catch (Exception exception) {
            throw exception;
        }
    }

    public boolean isInitiated() {
        return hikariDataSource != null;
    }

    public void close() {
        this.hikariDataSource.close();
    }

    /**
     * @return A new database connecting, provided by the Hikari pool.
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    /**
     * Create a new table in the database.
     *
     * @param name The name of the table.
     * @param info The table info between the round VALUES() brackets.
     */
    public void createTable(String name, String info) {
        new Thread(() -> {
            try (Connection resource = getConnection(); PreparedStatement statement = resource.prepareStatement("CREATE TABLE IF NOT EXISTS " + name + "(" + info + ");")) {
                statement.execute();
            } catch (SQLException exception) {

                if (spigot != null) {
                    spigot.getLogger().log(Level.WARNING, "An error occurred while creating database table " + name + ".");
                } else {
                    bungeecord.getLogger().log(Level.WARNING, "An error occurred while creating database table " + name + ".");
                }

                exception.printStackTrace();
            }
        }).start();
    }

    /**
     * Execute an update to the database.
     *
     * @param query  The statement to the database.
     * @param values The values to be inserted into the statement.
     */
    public void execute(String query, Object... values) {
        new Thread(() -> {
            try (Connection resource = getConnection(); PreparedStatement statement = resource.prepareStatement(query)) {
                for (int i = 0; i < values.length; i++) {
                    statement.setObject((i + 1), values[i]);
                }
                statement.execute();
            } catch (SQLException exception) {

                if (spigot != null) {
                    spigot.getLogger().log(Level.WARNING, "An error occurred while executing an update on the database.");
                    spigot.getLogger().log(Level.WARNING, "MySQL#execute : " + query);
                } else {
                    bungeecord.getLogger().log(Level.WARNING, "An error occurred while executing an update on the database.");
                    bungeecord.getLogger().log(Level.WARNING, "MySQL#execute : " + query);
                }

                exception.printStackTrace();
            }
        }).start();
    }

    /**
     * Execute an update to the database.
     *
     * @param query    The statement to the database.
     * @param finished Runnable execute once finished update.
     * @param values   The values to be inserted into the statement.
     */
    public void execute(String query, Runnable finished, Object... values) {
        new Thread(() -> {
            try (Connection resource = getConnection(); PreparedStatement statement = resource.prepareStatement(query)) {
                for (int i = 0; i < values.length; i++) {
                    statement.setObject((i + 1), values[i]);
                }
                statement.execute();
                finished.run();
            } catch (SQLException exception) {

                if (spigot != null) {
                    spigot.getLogger().log(Level.WARNING, "An error occurred while executing an update on the database.");
                    spigot.getLogger().log(Level.WARNING, "MySQL#execute : " + query);
                } else {
                    bungeecord.getLogger().log(Level.WARNING, "An error occurred while executing an update on the database.");
                    bungeecord.getLogger().log(Level.WARNING, "MySQL#execute : " + query);
                }

                exception.printStackTrace();
            }
        }).start();
    }

    /**
     * Execute a query to the database.
     *
     * @param query    The statement to the database.
     * @param callback The data callback (Async).
     * @param values   The values to be inserted into the statement.
     */
    public void select(String query, Consumer<ResultSet> callback, Object... values) {
        new Thread(() -> {
            try (Connection resource = getConnection(); PreparedStatement statement = resource.prepareStatement(query)) {
                for (int i = 0; i < values.length; i++) {
                    statement.setObject((i + 1), values[i]);
                }
                callback.accept(statement.executeQuery());
            } catch (SQLException exception) {

                if (spigot != null) {
                    spigot.getLogger().log(Level.WARNING, "An error occurred while executing a query on the database.");
                    spigot.getLogger().log(Level.WARNING, "MySQL#select : " + query);
                } else {
                    bungeecord.getLogger().log(Level.WARNING, "An error occurred while executing a query on the database.");
                    bungeecord.getLogger().log(Level.WARNING, "MySQL#select : " + query);
                }
                exception.printStackTrace();
            }
        }).start();
    }
}


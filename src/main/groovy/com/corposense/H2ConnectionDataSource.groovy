package com.corposense

import com.google.inject.Inject
import com.j256.ormlite.db.H2DatabaseType
import com.j256.ormlite.jdbc.DataSourceConnectionSource
import com.j256.ormlite.support.ConnectionSource

import javax.sql.DataSource

class H2ConnectionDataSource {
    final ConnectionSource connectionSource

    @Inject
    H2ConnectionDataSource(DataSource ds) {
        this.connectionSource = new DataSourceConnectionSource(ds, new H2DatabaseType())
    }
}
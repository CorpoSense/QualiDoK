package com.corposense

import com.corposense.models.Account
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import groovy.transform.CompileStatic
import ratpack.service.Service
import ratpack.service.StartEvent

@CompileStatic
class ConnectionInitializer implements Service {

        @Override
        void onStart(StartEvent event) {
            ConnectionSource connectionSource = event.registry.get(H2ConnectionDataSource).connectionSource
            TableUtils.createTableIfNotExists(connectionSource, Account)
        }

}

package com.corposense.services

import com.corposense.H2ConnectionDataSource
import com.corposense.models.Account
import com.google.inject.Inject
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import groovy.transform.CompileStatic
import ratpack.exec.Blocking
import ratpack.exec.Promise

@CompileStatic
class AccountService {
    final Dao<Account, String> accountDao

    @Inject
    AccountService(H2ConnectionDataSource connectionDataSource) {
        this.accountDao = DaoManager.createDao(connectionDataSource.connectionSource, Account) as Dao<Account, String>
    }

    Promise<Integer> create(Account account) {
        Blocking.get {
            accountDao.create(account)
        }
    }

    Promise<Account> get(String id) {
        Blocking.get {
            accountDao.queryForId(id)
        }
    }

    Promise<List<Account>> getAll() {
        Blocking.get {
            accountDao.queryForAll()
        }
    }

    Promise<Integer> delete(String id){
        Blocking.get {
            accountDao.deleteById(id)
        }
    }

    Promise<List<Account>> getActive(boolean active = true) {
        Blocking.get {
            accountDao.queryForEq('active', active)
        }
    }

}
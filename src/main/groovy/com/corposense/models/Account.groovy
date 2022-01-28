package com.corposense.models

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString

@DatabaseTable(tableName = "account")
@Canonical
@CompileStatic
class Account {
    @DatabaseField(generatedId = true)
    int id
    @DatabaseField
    String name
    @DatabaseField
    String url
    @DatabaseField
    String username
    @DatabaseField
    String password
}
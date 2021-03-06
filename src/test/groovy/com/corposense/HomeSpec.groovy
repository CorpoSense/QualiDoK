package com.corposense

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.MainClassApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HomeSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest groovyScriptApplicationUnderTest = new GroovyRatpackMainApplicationUnderTest()

    @Unroll
    def 'Response should return ok'() {
        when:
        def response = aut.httpClient.get()

        then:
        response.statusCode == 200

        where:
            aut                              | type
            groovyScriptApplicationUnderTest | 'ratpack.groovy'
    }


}

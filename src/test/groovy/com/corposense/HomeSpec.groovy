package com.corposense
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HomeSpec extends Specification {

    @AutoCleanup
    @Shared
    GroovyRatpackMainApplicationUnderTest app

    def setupSpec() {
        try {
            app = new GroovyRatpackMainApplicationUnderTest()
        } catch (Exception e) {
            e.printStackTrace()
            throw e
        }
    }

    @Unroll
    def 'Response should return ok'() {
        when:
        def response = aut.httpClient.get()

        then:
        response.statusCode == 200

        where:
            aut | type
            app | 'ratpack.groovy'
    }

}

package com.corposense
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HomeSpec extends Specification {

//    @AutoCleanup
//    @Shared
//    GroovyRatpackMainApplicationUnderTest app = new GroovyRatpackMainApplicationUnderTest()

    @Unroll
    def 'Response should return ok'() {
        given:
            int i = 1
        expect:
            i == 1

//        when:
//        def response = aut.httpClient.get()
//
//        then:
//        response.statusCode == 200
//
//        where:
//            aut | type
//            app | 'ratpack.groovy'
    }

}

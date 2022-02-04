import com.corposense.ConnectionInitializer
import com.corposense.H2ConnectionDataSource
import com.corposense.models.Account
import com.corposense.services.AccountService
import com.zaxxer.hikari.HikariConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.form.Form
import ratpack.hikari.HikariModule
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.thymeleaf3.ThymeleafModule
import static ratpack.groovy.Groovy.ratpack
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view
import static ratpack.jackson.Jackson.json
import static ratpack.jackson.Jackson.fromJson


final Logger log = LoggerFactory.getLogger(ratpack)

ratpack {
    serverConfig {
        development(true)
        port(3000)
        maxContentLength(26214400)
    }
    bindings {
        module (ThymeleafModule)
        module ( HikariModule, { HikariConfig config ->
            config.addDataSourceProperty("URL", "jdbc:h2:mem:account;INIT=CREATE SCHEMA IF NOT EXISTS DEV")
            config.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
        })
        bind (H2ConnectionDataSource)
        bind (AccountService)
        bindInstance (Service, new ConnectionInitializer())

        add Service.startup('startup'){ StartEvent event ->
            if (serverConfig.development){
                sleep(500)
                event.registry.get(AccountService)
                        .create(new Account(
                                name: 'Main Server',
                                url: 'http://127.0.0.1:8080',
                                username: 'admin',
                                password: 'admin',
                                active: true
                        )).then({ Integer id ->
                    log.info("Server N°: ${id} created.")
                })
            }
        }
    }
    handlers {

        get { AccountService accountService ->
            accountService.getActive().then({ List<Account> accounts ->
                Account account = accounts[0]
                if (accounts.isEmpty() || !account){
                    render(view("index", [message:'You must create a server account.']))
                } else {
                    render(view("index", ['account': account]))
                }
            })
        }

        prefix('upload') {
            // path('/pdf'){}
            all {
                byMethod {
                    get {
                        render(view("upload", [:]))
                    }
                }
            }
        }


        prefix('server') {

            path("delete") { AccountService accountService ->
                byMethod {
                    post {
                        parse(Form).then { Form map ->
                            accountService.delete(map['id']).then { Integer id ->
                                redirect('/server')
                            }
                        }
                    }
                }
            }

            path(':id'){ AccountService accountService ->
                byMethod {
                    get {
                        accountService.get(pathTokens['id']).then { Account account ->
                            render(json(account))
                        }
                    }
                }
            }

            all { AccountService accountService ->
                byMethod {
                    get {
                        accountService.all.then { List<Account> accounts ->
                            render(view('server', [servers: accounts]))
                        }
                    }
                    post {
                        parse(Form).then { Form map ->
                            accountService.create( new Account(map) ).then { Integer id ->
                                redirect('/server')
                            }
                        }
                    }
                } // byMethod
            } // all
        }

        // Serve assets from 'public'
        files { dir "static" }
    }
}
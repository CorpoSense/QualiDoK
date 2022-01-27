import ratpack.thymeleaf3.ThymeleafModule
import static ratpack.groovy.Groovy.ratpack
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view

ratpack {
    serverConfig {
        port(3000)
    }
    bindings {
        module (ThymeleafModule)
    }
    handlers {
        get{
            Date d = new java.util.Date()
            render(view("index", [user:'admin']))
        }
        // Serve assets from 'public'
        files { dir "public" }
    }
}
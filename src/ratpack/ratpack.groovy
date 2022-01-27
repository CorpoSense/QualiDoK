import ratpack.thymeleaf3.ThymeleafModule
import static ratpack.groovy.Groovy.ratpack
import static ratpack.thymeleaf3.Template.thymeleafTemplate as view

ratpack {
    bindings {
        module (ThymeleafModule)
    }
    handlers {
        get{
            render(view("index"))
        }
    }
}
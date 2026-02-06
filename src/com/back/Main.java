package com.app;
import com.app.controllers.UserController;
import org.reflections.Reflections;
import java.util.Set;
import java.lang.reflect.*;
import com.framework.annotation.*;

public class Main {
    public static void main(String[] args) {

        Class<UserController> clazz = UserController.class;
        System.out.println("Liste des URLs dans la classe " + clazz.getSimpleName() + " :");
        System.out.println("-------------------------------------------------------");
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(HandleUrl.class)) {
                HandleUrl annotation = method.getAnnotation(HandleUrl.class);
                System.out.println("Méthode : " + method.getName() + " --> URL : " + annotation.value());
            }
        }
        System.out.println("\n");

        Reflections reflections = new Reflections("com.app.controllers");
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
        System.out.println("Classes avec @Controller :");
        System.out.println("-------------------------------------------------------");
        for (Class<?> cls : controllers) {
            System.out.println(cls.getName());
        }
    }
}
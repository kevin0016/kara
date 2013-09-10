package kara

import java.io.File
import java.util.HashMap
import kara.internal.*
import java.net.URL
import java.util.ArrayList
import java.net.URLClassLoader

/**
 * Store application configuration.
 */
public open class AppConfig(val environment: String = "development", val appURL: URL? = null) : Config() {
    {
        this["kara.port"] = "8080"

        val cl = URLClassLoader(buildClasspath(), javaClass.getClassLoader())

        val configResolver: (jsonFile: String) -> URL? = {
            cl.getResource("config/$it")
        }

        // read the main appconfig file and also look for an environment-specific one
        configResolver("appconfig.json")?.let {ConfigReader(this).read(it)}
        configResolver("appconfig.${environment}.json")?.let {ConfigReader(this).read(it)}
    }

    /** Returns true if the application is running in the development environment. */
    fun isDevelopment() : Boolean {
        return environment == "development" || tryKey("kara.environment") == "development"
    }

    /** Returns true if the application is running in the test environment. */
    fun isTest() : Boolean {
        return environment == "test" || tryKey("kara.environment") == "test"
    }

    /** Returns true if the application is running in the production environment. */
    fun isProduction() : Boolean {
        return environment == "production" || tryKey("kara.environment") == "production"
    }

    public val appPackage : String
        get() = this["kara.appPackage"]

    public val appClass: String
        get() = if (contains("kara.appClass")) this["kara.appClass"] else "$appPackage.Application"

    /** The directory where publicly available files (like stylesheets, scripts, and images) will go. */
    public val publicDir : String
        get() = this["kara.publicDir"]

    public val routePackages: List<String>?
        get() {
            return tryKey("kara.routePackages")?.split(',')?.toList()?.map {"${it.trim()}"}
        }

    public val hotPackages: List<String>?
        get() {
            return tryKey("kara.hotPackages")?.split(',')?.toList()?.map {"${it.trim()}.*"}
        }

    public val staticPackages: List<String>?
        get() {
            return tryKey("kara.staticPackages")?.split(',')?.toList()?.map {"${it.trim()}.*"}
        }

    /** The port to run the server on. */
    public val port : String
        get() = this["kara.port"]


    public fun requestClassloader(current: ClassLoader): ClassLoader {
        if (isDevelopment()) {
            return URLClassLoader(buildClasspath(), current)
        }

        return current
    }

    public fun applicationClassloader(current: ClassLoader): ClassLoader {
        if (isDevelopment()) {
            val hot = hotPackages
            val static = staticPackages
            if (hot != null) {
                if (static == null)
                    return RestrictedClassLoader(hot, ArrayList<String>(), buildClasspath(), current)
                else
                    return RestrictedClassLoader(hot, static, buildClasspath(), current)
            }
        }

        return URLClassLoader(buildClasspath(), current)
    }

    protected open fun buildClasspath() : Array<URL> {
        if (appURL == null) {
            return Array<URL>(0) {URL("")}
        }
        else {
            return Array<URL>(1) {appURL}
        }
    }
}

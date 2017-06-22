// FIXME: somehow if that test file doesnt exist
//        the tests for the routes cant resolve the
//        classes
package de.martaflex.nanopdf

import java.util.concurrent.TimeUnit

import org.junit.*
import org.junit.Assert.*

import spark.Spark.*
import com.mashape.unirest.http.*

class ServiceTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup () {
            Service();
            // wait for spark to be initialized
            awaitInitialization()
        }

        @AfterClass @JvmStatic
        fun teardown () {
            stop();
            // give spark some time to shut down
            // FIXME: there is not method for waiting til spark server is stopped
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    fun noTest () {
    }

}


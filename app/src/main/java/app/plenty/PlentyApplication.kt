package app.plenty

import android.app.Application
import app.plenty.data.GardenRepository
import app.plenty.data.PlentyDatabase

class PlentyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Process-wide singletons. Small enough that a DI framework would add more than it saves. */
class AppContainer(application: Application) {
    val database: PlentyDatabase = PlentyDatabase.create(application)
    val gardenRepository = GardenRepository(database)
}


# ewgf-gg Backend

This is the backend service that runs https://www.ewgf.gg/. It is designed to collect, analyze, and serve Tekken 8 Ranked battle data, served from the [Wavu Wank api](https://wank.wavu.wiki/api). This service will poll the Wavu api for new data, and update the relevant tables within the database. 

## Technologies used
![java](https://github.com/user-attachments/assets/b199be0a-1d89-404b-8ba8-c1f2bf399a99) ![spring-boot](https://github.com/user-attachments/assets/4b94f768-a3bf-4faa-8fc8-c05b2e324b0e) ![postgresql(1)](https://github.com/user-attachments/assets/5d1fd3f9-742e-42ef-bfc5-42ed60954938) ![docker(1)](https://github.com/user-attachments/assets/141d79d6-38e9-426d-9c52-1e464da5eddb) ![rabbitmq(2)](https://github.com/user-attachments/assets/3fa507a4-2fc9-4d80-accd-5dad79a3e774)


## Current Production Architecture (as of 01/19/2025)
![Tekken Diagrams(1)](https://github.com/user-attachments/assets/acec0bdd-6d5e-4474-913b-59fa1f038183)

## Data Flow

1. Battle data is fetched by `WavuService.java`
2. Battle data is published to RabbitMQ in batches of 6000 - 12000
3. BattleProcessingService listens for new messages from RabbitMQ, processes batches
4. Events are published when new data is processed
5. Statistics are re-calculated in
6. Frontend requests data through REST endpoints
7. Controllers retrieve and format the data for presentation

## Local Development Setup

1. Clone this repository
2. In your environment configuration, set your active profile to "Dev"
3. Install and run the latest version of [Docker Desktop](https://www.docker.com/products/docker-desktop/#)
4. The service will automatically download the required dependencies (RabbitMQ and PostgreSQL v17)
5. Run the commands from `init.sql` within the containerized postgres database
6. Run the application using your preferred IDE or via command line: ./mvnw spring-boot:run

## Lessons Learned
### Concurrency & Multithreading
Dealing with race conditions and deadlocks was a significant challenge. Key solutions included:
* Sorting batches prior to insertion
* Leveraging virtual threads for better resource utilization
* Upsert operations eliminated race conditions by atomically modifying rows
* Incrementing wins/losses directly instead of read->modify->insert dramatically reduced database reads, eliminated issues with stale data from having multiple threads target the same rows
* Using JDBC for more granular control over database operations

JPA vs JDBC Tradeoffs
Discovered limitations with JPA for high-throughput operations:

* Default JPA methods weren't batched or executed efficiently
JDBC provided more control for optimizing database interactions
Custom query implementations delivered better performance for specific operations


## Changelog

* **03/18/2024:** Website has been running successfully for a few months now! Though there were some speedbumps along the way:
  * There were race conditions still occuring in the character_stats table, most likely Time-of-check-to-time-of-use. This only occurs during the initial preload (as that is when the database is under the heaviest load), so I added a revalidator that will manually recalculate all character stats until I could implement a more permanent solution.
  * Fixed a bug with the fetching of battles. Prior to this, the fetching logic was allowing a small window of time to go unaccounted for.
  * Cleaned up some of the codebase and created some static utils classes. 
 
* **12/23/2024:** So many updates! This is pretty close to a production build, there's just a few more loose ends to clean up. Probably also should write tests at some point, too. 
  * Moved the event publisher to its own class to avoid coupling unrelated classes together
  * Added new endpoints to retrieve versioned + regioned statistics
  * Deleted a lot of unused classes + methods 
  * Created tons of new DTOs + projections for the versioned statistics
  * Squashed hidden bug in the aggregatedstatisticId class where region and area were not being considered in `hashcode()` and `equals()` methods
  * Enhanced the logic for determining a 'main' character of a player.
  * Squashed bug with keeping accurate player counts. 
  * Abstracted the calculations logic from the StatisticsController class to a new CharacterAnalyticsService class.
  * Added support for Clive
  * Updated application.yml file. 
  


* **11/22/2024:** Beginning work on retrieving player match history when a player is queried. Statistics events now contain game versions, so that only relevant game version statistics are re-calculated. 


* **11/08/2024:** Statistics analysis is now completely event driven. Server also now retrieves top 5 statistics for high, medium and low ranks.


* **11/07/2024:** Began work on making statistics analysis trigger on event basis instead of being done on a scheduled/cron job basis. 


* **11/06/2024:** Added support for calculating winrates and popularity for top 5 ranks.


* **11/05/2024:** Added support for region and areas for statistics aggregation, added region, area, and language as part of battle and player models. Added class that manages configuration for virtual threads. Deleted Custom row mapper class (will return to this in the future) Added status checking for responses from API. Switched from field to constructor injection in RabbitService, APIService, and StatisticService classes. Started work on adding region and area for statistics aggregation.

  
* **10/19/2024:** REST API endpoints have been created and are functioning perfectly! Average response times seem to be anywhere between 50-100ms which is amazing. Can't wait to see what it's like when everything is deployed.


* **10/15/2024:** Character Stats are now separated by game version. This hopefully will provide a more accurate look at characters after they have been adjus


* **10/12/2024:** Removed Bloom filter along with associated dependency. Returned to previous method of queueing for existence checks, but with a much more limited scope. Heavily refactored APIService class to support full dynamic loading of databases. APIService can now continue fetching historical data without the need for manual intervention. It will also switch to begin fetching forward once database preload is complete. Refactored JSON Parser, but still need to implement custom parser to not rely on Type Inference.


* **10/11/2024:** Removed the hardcoded timestamp value and let the server retrieve them dynamically. Refactored all model classes to fit new schema naming scheme.


* **10/10/2024:** Cleaned up pom.xml, added virtual thread functionality


* **10/08/2024:** Renamed classes to better suit their functionality. Squashed a bug that was causing wins and losses to not be recorded accurately in the database. Deleted a bunch of error log files that were generated when I was testing for deadlocks. Added a class to map rows to battle class.


* **10/05/2024:** Implemented proper use of upserts, which allowed me to remove the need for most existence checks/reads altogether. Changed Postgres schema to remove duplicate columns.


* **10/04/2024:** Reintroduced batched reads for Postgres. Implemented Bloom Filter cache in an effort to reduce the amount of reads made to the database.


* **10/03/2024:** Implemented pagination for batched upserts.


* **09/30/2024:** Migration from MongoDB to PostgreSQL. Squashed bug with that caused TekkenPower to not update correctly. Player class no longer has the character_stats class nested within. Player, Character_Stats, and Past_player_names are now their own classes and entities.


* **09/25/2024:** Added backpressure functionality to RabbitMQ


* **09/20/2024:** Removed PlayerDocument class, refactored Player class to include a nested character_stats class. Enabled retries incase a transaction were to fail.


* **09/19/2024:** Cleaned up logger messages, they now display correct INFO/WARN/ERRORs.


* **09/17/2024:** Refactored `ProcessReplays()` by splitting off into several helper methods.


* **09/15/2024:** Enabled multithreading and RabbitMQ as a message queue service. Added benchmark logs. Switched from `saveAll()` to `bulkOps` for true batched inserts and reads.


* **09/05/2024:** Added benchmark logs. Implemented batched inserts and reads.


* **09/04/2024:** Squashed bug with saving past player names correctly, Squashed a separate bug with last 10 battles not showing up, and ratings.


* **09/03/2024:** FIRST RUN! Completed replayservice class.


* **08/29/2024:** First query to mongoDB successful! Added player class and set up database connections. 




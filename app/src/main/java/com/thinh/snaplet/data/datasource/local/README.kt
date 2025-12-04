package com.thinh.snaplet.data.datasource.local

/**
 * Local Data Source - Placeholder for future implementation
 * 
 * This folder is prepared for future Room database implementation when needed.
 * 
 * When implementing local data, you will need:
 * 
 * 1. Entity (Database Table):
 *    - MediaEntity.kt
 *    - Define @Entity with table structure
 * 
 * 2. DAO (Data Access Object):
 *    - MediaDao.kt
 *    - Define @Query for CRUD operations
 * 
 * 3. Database:
 *    - AppDatabase.kt
 *    - @Database with entities list
 * 
 * 4. Mapper (Optional):
 *    - Convert MediaEntity <-> MediaItem
 * 
 * 5. Dependencies (add to build.gradle.kts):
 *    - androidx.room:room-runtime
 *    - androidx.room:room-ktx
 *    - androidx.room:room-compiler (ksp)
 * 
 * 6. DI Module:
 *    - LocalDataModule.kt
 *    - Provide AppDatabase, DAOs
 * 
 * For now, only remote data (API) is used via MediaRepositoryImpl.
 */

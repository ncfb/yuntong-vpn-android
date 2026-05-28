package com.yuntong.vpn.model

import androidx.room.*

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val serverPort: Int = port,
    val uuid: String = "",
    val encryption: String = "",
    val network: String = "tcp",
    val tls: Int = 0,
    val sni: String = "",
    val path: String = "",
    val cipher: String = "",
    val protocol: String = "",
    val protocolParam: String = "",
    val obfs: String = "",
    val obfsParam: String = "",
    val groupName: String = "",
    val isActive: Boolean = false,
    val latency: Int = -1,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("UPDATE profiles SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Int)
}

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}

// Extension: Entity -> Model
fun ProfileEntity.toModel() = VpnProfile(
    id = id,
    name = name,
    type = type,
    host = host,
    port = port,
    serverPort = serverPort,
    uuid = uuid,
    encryption = encryption,
    network = network,
    tls = tls,
    sni = sni,
    path = path,
    cipher = cipher,
    protocol = protocol,
    protocolParam = protocolParam,
    obfs = obfs,
    obfsParam = obfsParam,
    groupName = groupName,
    isActive = isActive,
    latency = latency
)

// Extension: Model -> Entity
fun VpnProfile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    type = type,
    host = host,
    port = port,
    serverPort = serverPort,
    uuid = uuid,
    encryption = encryption,
    network = network,
    tls = tls,
    sni = sni,
    path = path,
    cipher = cipher,
    protocol = protocol,
    protocolParam = protocolParam,
    obfs = obfs,
    obfsParam = obfsParam,
    groupName = groupName,
    isActive = isActive,
    latency = latency
)

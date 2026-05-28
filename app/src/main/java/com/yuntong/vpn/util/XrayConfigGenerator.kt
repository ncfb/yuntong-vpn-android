package com.yuntong.vpn.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.yuntong.vpn.model.VpnProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates Xray-core JSON config from a VpnProfile.
 * Supports AnyTLS, VLESS, Hysteria2, ShadowsocksR protocols.
 */
object XrayConfigGenerator {

    fun generate(profile: VpnProfile): String {
        val config = JSONObject()

        // Log config
        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // DNS config
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                add(JSONObject().apply {
                    put("address", "8.8.8.8")
                    put("port", 53)
                })
                add(JSONObject().apply {
                    put("address", "1.1.1.1")
                    put("port", 53)
                })
            })
        })

        // Inbounds (tun)
        config.put("inbounds", JSONArray().apply {
            add(JSONObject().apply {
                put("tag", "tun-in")
                put("protocol", "dokodemo-door")
                put("listen", "127.0.0.1")
                put("settings", JSONObject().apply {
                    put("network", "tcp,udp")
                    put("followRedirect", true)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        add("http")
                        add("tls")
                        add("quic")
                    })
                })
                put("streamSettings", JSONObject().apply {
                    put("sockopt", JSONObject().apply {
                        put("tproxy", "tun")
                    })
                })
            })
        })

        // Outbounds
        config.put("outbounds", JSONArray().apply {
            add(createOutbound(profile))
            // Direct outbound for blocked apps
            add(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })
            // Block outbound
            add(JSONObject().apply {
                put("tag", "block")
                put("protocol", "blackhole")
            })
        })

        // Routing
        config.put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray().apply {
                // Direct for private IPs
                add(JSONObject().apply {
                    put("type", "field")
                    put("ip", JSONArray().apply {
                        add("geoip:private")
                    })
                    put("outboundTag", "direct")
                })
            })
        })

        return config.toString(2)
    }

    private fun createOutbound(profile: VpnProfile): JSONObject {
        return when (profile.type) {
            "anytls" -> createAnytlsOutbound(profile)
            "vless" -> createVlessOutbound(profile)
            "hysteria2" -> createHysteria2Outbound(profile)
            "shadowsocksr" -> createSSROutbound(profile)
            else -> createVlessOutbound(profile)
        }
    }

    private fun createAnytlsOutbound(profile: VpnProfile): JSONObject {
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "anytls")
            put("settings", JSONObject().apply {
                put("serverName", profile.sni.ifEmpty { profile.host })
            })
            put("streamSettings", JSONObject().apply {
                put("network", "raw")
                put("security", "none")
                put("rawSettings", JSONObject().apply {
                    put("headerType", "none")
                })
            })
            put("sendThrough", "0.0.0.0")
            put("address", profile.host)
            put("port", profile.serverPort)
        }
    }

    private fun createVlessOutbound(profile: VpnProfile): JSONObject {
        val streamSettings = JSONObject().apply {
            put("network", profile.network)
            if (profile.tls == 1) {
                put("security", "tls")
                put("tlsSettings", JSONObject().apply {
                    put("serverName", profile.sni.ifEmpty { profile.host })
                    put("allowInsecure", true)
                    put("fingerprint", "chrome")
                })
            } else {
                put("security", "none")
            }
            if (profile.network == "ws") {
                put("wsSettings", JSONObject().apply {
                    put("path", profile.path.ifEmpty { "/" })
                    if (profile.headers.isNotEmpty()) {
                        put("headers", JSONObject(profile.headers))
                    }
                })
            }
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    add(JSONObject().apply {
                        put("address", profile.host)
                        put("port", profile.serverPort)
                        put("users", JSONArray().apply {
                            add(JSONObject().apply {
                                put("id", profile.uuid)
                                put("encryption", "none")
                                put("flow", "xtls-rprx-vision")
                            })
                        })
                    })
                })
            })
            put("streamSettings", streamSettings)
        }
    }

    private fun createHysteria2Outbound(profile: VpnProfile): JSONObject {
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "hysteria2")
            put("settings", JSONObject().apply {
                put("serverName", profile.sni.ifEmpty { profile.host })
                if (profile.uuid.isNotEmpty()) {
                    put("password", profile.uuid)
                }
                put("tls", JSONObject().apply {
                    put("serverName", profile.sni.ifEmpty { profile.host })
                    put("insecure", false)
                })
            })
            put("sendThrough", "0.0.0.0")
            put("address", profile.host)
            put("port", profile.serverPort)
        }
    }

    private fun createSSROutbound(profile: VpnProfile): JSONObject {
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "shadowsocksr")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply {
                    add(JSONObject().apply {
                        put("address", profile.host)
                        put("port", profile.serverPort)
                        put("method", profile.encryption)
                        put("password", profile.uuid)
                        put("protocol", profile.protocol.ifEmpty { "origin" })
                        put("protocolParam", profile.protocolParam)
                        put("obfs", profile.obfs.ifEmpty { "plain" })
                        put("obfsParam", profile.obfsParam)
                    })
                })
            })
        }
    }

    fun profileToJson(profile: VpnProfile): String = Gson().toJson(profile)
    fun profileFromJson(json: String): VpnProfile = Gson().fromJson(json, VpnProfile::class.java)
}

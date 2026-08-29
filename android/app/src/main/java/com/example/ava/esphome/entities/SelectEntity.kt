package com.example.ava.esphome.entities

import android.util.Log
import com.example.esphomeproto.api.EntityCategory
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.SelectCommandRequest
import com.example.esphomeproto.api.listEntitiesSelectResponse
import com.example.esphomeproto.api.selectStateResponse
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SelectEntity(
    val key: Int,
    val name: String,
    val objectId: String,
    val icon: String = "",
    private val options: () -> List<String>,
    private val getState: Flow<String>,
    val entityCategory: EntityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
    private val setState: suspend (String) -> Boolean
) : Entity {
    override fun handleMessage(message: MessageLite): Flow<MessageLite> = flow {
        when (message) {
            is ListEntitiesRequest -> emit(listEntitiesSelectResponse {
                key = this@SelectEntity.key
                name = this@SelectEntity.name
                objectId = this@SelectEntity.objectId
                options.addAll(this@SelectEntity.options())
                if (this@SelectEntity.icon.isNotEmpty()) icon = this@SelectEntity.icon
                entityCategory = this@SelectEntity.entityCategory
            })

            is SelectCommandRequest -> if (message.key == key) {
                Log.d(TAG, "SelectCommand received: key=$key, objectId=$objectId, state=${message.state}")
                if (setState(message.state)) {
                    emit(selectStateResponse {
                        key = this@SelectEntity.key
                        state = message.state
                        missingState = false
                    })
                }
            }
        }
    }

    override fun subscribe() = getState.map {
        selectStateResponse {
            key = this@SelectEntity.key
            state = it
            missingState = false
        }
    }

    companion object {
        private const val TAG = "SelectEntity"
    }
}

package io.github.sunshinewzy.shining.api.data.container

import io.github.sunshinewzy.shining.api.data.IDataRoot
import io.github.sunshinewzy.shining.api.namespace.NamespacedId

/**
 * Represent a container of data
 *
 *
 * 表示数据容器
 */
interface IDataContainer {

    /**
     * Get the requested [IDataRoot] by [key].
     *
     *
     * 通过 [key] 获取一个 [IDataRoot]。
     *
     * @param key Key of the [IDataRoot] to get.
     * @return Requested [IDataRoot].
     */
    operator fun get(key: NamespacedId): IDataRoot

}
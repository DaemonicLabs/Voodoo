package voodoo

import voodoo.core.data.flat.ModPack
import voodoo.core.data.lock.LockPack

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 * @version 1.0
 */

fun ModPack.lock(): LockPack {
    return LockPack(
            name = name,
            title = title,
            mcVersion = mcVersion,
            forge = forge,
            entries = entries.map { versions[it.name]!! },
            features = features
    )
}
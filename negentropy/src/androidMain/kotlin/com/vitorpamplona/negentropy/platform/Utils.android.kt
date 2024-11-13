package com.vitorpamplona.negentropy.platform

import java.security.MessageDigest

actual fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
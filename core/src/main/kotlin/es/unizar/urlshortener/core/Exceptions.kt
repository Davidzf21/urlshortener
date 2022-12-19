package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

class UpdateValidationNoWork(val key: String) : Exception("[$key] validation can not be update")

class RedirectionNotSafeOrBlock(val key: String) : Exception("Hash [$key] redirection block. Page is block or not safe")

class ShortUrlNotSafe(val key: String) : Exception("Page [$key] is not safe")

class RedirectionNotReachable(val key: String) : Exception("Hash [$key] redirection fail. Page is not reachable")

class InvalidFileType(): Exception("The format is not supported, only .csv files")



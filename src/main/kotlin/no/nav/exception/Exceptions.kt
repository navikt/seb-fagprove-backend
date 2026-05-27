package no.nav.exception

class NotFoundException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

package com.example.mfservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MfServiceApplication

fun main(args: Array<String>) {
    runApplication<MfServiceApplication>(*args)
}

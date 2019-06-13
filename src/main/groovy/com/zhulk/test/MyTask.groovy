package com.zhulk.test

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class MyTask extends DefaultTask {
    String input = 'hello from MyTask'

    @TaskAction
    def greet() {
        println input
    }
}

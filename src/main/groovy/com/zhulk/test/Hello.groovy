package com.zhulk.test

import org.gradle.api.Plugin
import org.gradle.api.Project


class Hello implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // 向extension container保存para参数
        project.extensions.create("para", HelloPluginExtension)
        // 向project对象添加hello任务
        project.task('hello',type:MyTask) {
            // 设置greeting参数
            input = 'Hello Plugin input!'

            doLast {
                println "${project.para.first}${project.para.last}"
            }
        }

    }
}

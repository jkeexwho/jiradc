package com.autodoc.scripts.groovy.pops.autoUserManagement

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.user.UserService

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userManager = ComponentAccessor.getUserManager()
def userService = ComponentAccessor.getComponent(UserService)

def userEmailField = customFieldManager.getCustomFieldObject("customfield_18100")
if (userEmailField) {
    def userEmail = issue.getCustomFieldValue(userEmailField)?.toString()?.replaceAll("\\s", "")
    if (userEmail) {
        def user = userManager.getUserByName(userEmail)
        if (user && !user.isActive()) {
            def updateUser = userService.newUserBuilder(user).active(true).build()
            def updateUserValidationResult = userService.validateUpdateUser(updateUser)

            if (updateUserValidationResult.valid) {
                userService.updateUser(updateUserValidationResult)
            }
        }
    }
}



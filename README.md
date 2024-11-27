## Employee directory organization

This is an application for managing employees of a company. Employees belong to organizations within the company.

As recognition, employees can receive Dundie Awards.

* A `Dundie Award` is in reference to the TV show [The Office](https://en.wikipedia.org/wiki/The_Dundies) in which the main character hands out awards to his colleagues. For our purposes, it's a generic award.

## Instructions

In preparation for the upcoming call with NinjaOne, `clone` this repo and run it locally. If everything runs successfully, you will see the following page in your browser.

![success](success.png)

Become familiar with the application and it's characteristics. Use your favorite HTTP Client (like [Postman](https://www.postman.com/)) to exercise the endpoints and step through the code to help you get to know the application. 

In the call, we will introduce new code to the application, and you will comment on issues with the endpoint. Please be ready to share your screen in the call with us with the application ready to run. 

**Bonus:** Spot any issues or potential improvements you notice in the application while you're familiarizing yourself and make note of them for our call. We would love to see your input in how to make this application better.

## Give Dundie Awards to Organization


### Features

**Implement the Endpoint**  
Create the endpoint "/give-dundie-awards/{organizationId}". This endpoint should increase the number of Dundie awards for each employee in the specified organization by 1. Ensure that any related updates are also handled appropriately.  

**Finish Message Broker Implementation**  
Complete the implementation of the Message Broker by either introducing a library or creating a basic publish/subscribe mechanism.  

**Asynchronous Activity Creation**  
Implement the creation of an Activity when awards are added to an organization. This should be done asynchronously by subscribing to notifications from the Message Broker.  

**Implement Rollback Mechanism**  
Develop a mechanism to roll back the award distribution if the Activity creation fails.  

### Sequence Diagram

![Give Dundie Awards to Organization](Give Dundie Awards to Organization.svg)

### Sequence Diagram source

```
title Give Dundie Awards to Organization


#CONTROLLER
User->AwardController: /give-dundie-awards/{organizationId}
alt #a3001f Organization is valid
AwardController->AwardController: Bean validate \n@ValidOrganizationId, \n@BlockedOrganizationId
AwardController->OrganizationService: isBlocked(organizationId)\nexistsOrganization(organizationId)
    AwardController->AwardController: generate Request UUID
    AwardController->AwardService: giveDundieAwards(UUID,long organizationId)
    AwardService->EmployeeService: getEmployeesIdsByOrganization(long organizationId)
    EmployeeService->AwardService:List<long> ids
    AwardService->AwardService:chunkIntoBatches(List<long>)
    loop for each List<long> batch in batches
        AwardService->EmployeeService: addDundieAwardToEmployees(batch)
        EmployeeService->EmployeeRepository: increaseAwardsToEmployees(List<long>)
        AwardService->AwardBatchLogService: saveAwardBatchLogs(UUID,List<long>)
    end
    AwardService->OrganizationService:blockOrganization(UUID,organizationId)
    OrganizationService->OrganizationRepository:blockOrganizationById(UUID,organizationId)\nChange:\norganization.blocked:true\norganization.blocked_by:UUID
    AwardService->AwardCache: increseAwards(totalAwards)
    AwardService->MessageBroker: publish(AWARD_ORGANIZATION_SUCCESS_EVENT[UUID, instant, totalAffectedEmployees, totalAwards])
    AwardService->AwardController: Success    
    AwardController->User: Ok 200: \nAwarded Organization
else Organization is blocked
    AwardController->User: Error 500: \nOrganization is blocked
else Organization is blocked
    AwardController->User: Error 404: \nOrganization ID is not Valid
end


MessageBroker-->StreamProcessor:process(AWARD_ORGANIZATION_SUCCESS_EVENT[UUID, instant, totalAffectedEmployees, totalAwards])
StreamProcessor->ActivityService:handleAwardOrganizationSuccess(AWARD_ORGANIZATION_SUCCESS_EVENT)

# REPEATABLE SAVE ACTIVITY
ActivityService->ActivityService:repeatableSaveActivity(UUID)
group #1f77b4 repeatableSaveActivity(UUID) #white
    ActivityService->ActivityService: toActivity(AWARD_ORGANIZATION_SUCCESS_EVENT)
    loop Retry until success or ENV{retrySaveActivitiesMaxAttempts}
        ActivityService->ActivityService: await(attempt * ENV{timeToWaitRetry})
        alt Activity save success
            ActivityService->ActivityRepository: saveActivity(Activity)
            ActivityService->MessageBroker: publish(SAVE_ACTIVITY_AWARD_ORGANIZATION_SUCCESS_EVENT[UUID, instant, Activity])
        else Retry save failure
            ActivityService->ActivityService: increaseAttempt
            ActivityService->MessageBroker: publish(SAVE_ACTIVITY_AWARD_ORGANIZATION_RETRY_EVENT[UUID, instant, attempt, Activity])
        else Reach max attempts: ENV{retrySaveActivitiesMaxAttempts}
            ActivityService->MessageBroker: publish(SAVE_ACTIVITY_AWARD_ORGANIZATION_FAILURE_EVENT[UUID, instant, Activity])
        end
    end
end


MessageBroker-->StreamProcessor:process(SAVE_ACTIVITY_AWARD_ORGANIZATION_RETRY_EVENT[UUID, instant, attempt, Activity])
StreamProcessor->ActivityService:handleSaveActivityAwardOrganizationRetry(SAVE_ACTIVITY_AWARD_ORGANIZATION_RETRY_EVENT)
ActivityService->ActivityService:repeatableSaveActivity(UUID)
ref over ActivityService, MessageBroker #D4E4F0: Repeatable Save Activity Process


MessageBroker-->StreamProcessor: process(SAVE_ACTIVITY_AWARD_ORGANIZATION_SUCCESS_EVENT[UUID, instant, Activity])
StreamProcessor->AwardService: handleSuccessSaveActivityAwardOrganization(SAVE_ACTIVITY_AWARD_ORGANIZATION_SUCCESS_EVENT)
AwardService->OrganizationService:retryableUnblockOrganization(UUID,organizationId)
AwardService->AwardCache: decreaseAwards(totalAwards)


# REPEATABLE ORGANIZATION UNBLOCK
group #ff7f0e repeatableUnblockOrganization(UUID) #white
    loop Retry until success or ENV{retryUnblockOrganizationsMaxAttempts}
        OrganizationService->OrganizationService: await(attempt * ENV{timeToWaitRetry})
        alt Organization unblock success
            OrganizationService->OrganizationRepository: unblockOrganization(Organization)
            OrganizationService->MessageBroker: publish(UNBLOCK_ORGANIZATION_SUCCESS_EVENT[UUID, instant, Organization])
        else Retry unblock failure
            OrganizationService->OrganizationService: increaseAttempt
            OrganizationService->MessageBroker: publish(UNBLOCK_ORGANIZATION_RETRY_EVENT[UUID, instant, attempt, Organization])
        else Reach max attempts: ENV{retryUnblockOrganizationsMaxAttempts}
            OrganizationService->MessageBroker: publish(UNBLOCK_ORGANIZATION_FAILURE_EVENT[UUID, instant, Organization])
        end
    end
end

MessageBroker-->StreamProcessor: process(UNBLOCK_ORGANIZATION_RETRY_EVENT[UUID, instant, attempt, Organization])
StreamProcessor->OrganizationService: handleUnblockOrganizationRetry(UNBLOCK_ORGANIZATION_RETRY_EVENT)
ref over OrganizationService, MessageBroker #FCE6D0: Repeatable Organization Unblock Process

MessageBroker-->StreamProcessor: process(UNBLOCK_ORGANIZATION_SUCCESS_EVENT[UUID, instant, Organization])
StreamProcessor->OrganizationService: handleUnblockOrganizationSuccess(UNBLOCK_ORGANIZATION_SUCCESS_EVENT)
OrganizationService->AwardBatchLogService:cleanAwardBatchLogs(UUID)

MessageBroker-->StreamProcessor: process(UNBLOCK_ORGANIZATION_FAILURE_EVENT[UUID, instant, Organization])
StreamProcessor->OrganizationService: handleUnblockOrganizationFailure(UNBLOCK_ORGANIZATION_FAILURE_EVENT)
OrganizationService->FailureProcessService: logFailureProcess(UUID)



MessageBroker-->StreamProcessor: process(SAVE_ACTIVITY_AWARD_ORGANIZATION_FAILURE_EVENT)
StreamProcessor->AwardService: handleSaveActivityAwardOrganizationFailure(SAVE_ACTIVITY_AWARD_ORGANIZATION_FAILURE_EVENT)


# REPEATABLE ROLLBACK
group #2f2e7b repeatableRollback(UUID) #white
    loop Retry until success or ENV{retryAwardRollbackMaxAttempts}
        alt Award rollback success
            AwardService->AwardService: await(attempt * ENV{timeToWaitRetry})
            group rollback(UUID)
                AwardService->AwardBatchLogService: getAwardBatchLogs(UUID)
                AwardRepository->AwardService: List<BatchLogs>
                loop for each List<long> batch in batches
                    AwardService->EmployeeService: removeDundieAwardToEmployees(batch)
                    EmployeeService->EmployeeRepository: decreaseAwardsToEmployees(List<long>)
                end
            end
            AwardService->MessageBroker: publish(AWARD_ORGANIZATION_ROLLBACK_SUCCESS_EVENT[UUID, instant, totalAffectedEmployees, totalAwards])
        else Award rollback failure
            AwardService->AwardService: increaseAttempt
            AwardService->MessageBroker: publish(AWARD_ORGANIZATION_ROLLBACK_RETRY_EVENT[UUID, instant, attempt])
        else Reach max attempts: ENV{retryAwardRollbackMaxAttempts}
            AwardService->MessageBroker: publish(AWARD_ORGANIZATION_ROLLBACK_FAILURE_EVENT[UUID, instant])
        end
    end
end

MessageBroker-->StreamProcessor: process(AWARD_ORGANIZATION_ROLLBACK_RETRY_EVENT[UUID, instant, attempt])
StreamProcessor->AwardService: handleAwardOrganizationRollbackRetry(AWARD_ORGANIZATION_ROLLBACK_RETRY_EVENT)
ref over AwardService, MessageBroker #D6D5E5 : Repeatable Rollback Process


MessageBroker-->StreamProcessor: process(AWARD_ORGANIZATION_ROLLBACK_SUCCESS_EVENT)
StreamProcessor->AwardService: handleAwardOrganizationRollBackSuccess(AWARD_ORGANIZATION_ROLLBACK_SUCCESS_EVENT)
AwardService->OrganizationService:retryableUnblockOrganization(UUID,organizationId)
ref over OrganizationService,MessageBroker #FCE6D0:Referencing Organization Unblock Flow
AwardService->AwardCache: decreaseAwards(totalAwards)

MessageBroker-->StreamProcessor: process(AWARD_ORGANIZATION_ROLLBACK_FAILURE_EVENT[UUID, instant])
StreamProcessor->AwardService: handleAwardOrganizationRollbackFailure(AWARD_ORGANIZATION_ROLLBACK_FAILURE_EVENT)
AwardService->FailureProcessService:logFailureProcess(UUID)

```

Source: [https://sequencediagram.org/](https://sequencediagram.org/)
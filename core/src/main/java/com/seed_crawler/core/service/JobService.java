package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;
import com.seed_crawler.core.dto.command.JobDeleteCommand;
import com.seed_crawler.core.dto.command.JobOperationCommand;
import com.seed_crawler.core.dto.command.JobStatusCommand;
import com.seed_crawler.core.dto.command.JobUpdateCommand;

public interface JobService {
    JobDto.SaveResult saveJob(JobCreationCommand command);
    JobDto.UpdateResult updateJob(JobUpdateCommand command);
    JobDto.DeleteResult deleteJob(JobDeleteCommand command);
    JobDto.StatusResult updateJobStatus(JobStatusCommand command);
    JobDto.OperationResult operateJob(JobOperationCommand command);
    JobDto.OperationResult stopJob(JobOperationCommand command);
}

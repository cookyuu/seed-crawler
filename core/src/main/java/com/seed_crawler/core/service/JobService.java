package com.seed_crawler.core.service;

import com.seed_crawler.core.dto.JobDto;
import com.seed_crawler.core.dto.command.JobCreationCommand;

public interface JobService {
    JobDto.SaveResult saveJob(JobCreationCommand command);
}

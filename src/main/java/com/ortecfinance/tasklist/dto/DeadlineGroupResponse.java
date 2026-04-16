package com.ortecfinance.tasklist.dto;

import java.util.List;

/**
 * DTO representing a chronological grouping of tasks sent in an API response.
 * Used primarily for the 'view_by_deadline' endpoint.
 *
 * @param deadline the formatted shared deadline for this group, or null if no deadline
 * @param projects the list of projects containing tasks due on this date
 */
public record DeadlineGroupResponse(String deadline, List<ProjectResponse> projects) {
}

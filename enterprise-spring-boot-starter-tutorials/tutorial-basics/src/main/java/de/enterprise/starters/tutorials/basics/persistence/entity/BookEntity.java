package de.enterprise.starters.tutorials.basics.persistence.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *
 * @author Jonas Keßler
 */
@Data
public class BookEntity {

	private Long id;

	@ApiModelProperty("Should not contain blacklisted strings")
	private String title;

}

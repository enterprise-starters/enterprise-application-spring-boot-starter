package de.enterprise.starters.mongodb.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spring Data Mongodb can not map empty aggregation results correctly to Optional or primitive data types.
 *
 * @author Malte Geßner
 * @param <T>
 *            wrapped type
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SimpleResultWrapper<T> {
	private T value;
}

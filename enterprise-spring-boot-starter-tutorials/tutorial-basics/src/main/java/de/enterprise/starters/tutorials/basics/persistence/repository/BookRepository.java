package de.enterprise.starters.tutorials.basics.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Service;

import de.enterprise.starters.tutorials.basics.persistence.entity.BookEntity;

/**
 * Mock implementation of a book repository.
 *
 * @author Jonas Keßler
 */
@Service
public class BookRepository {

	public BookEntity save(BookEntity book) {
		book.setId(1L);
		return book;
	}

	public Optional<BookEntity> findById(Long id) {
		if (id == 1L) {
			return Optional.of(new BookEntity());
		}
		return Optional.ofNullable(null);
	}

}

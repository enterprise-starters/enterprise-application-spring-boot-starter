package de.enterprise.starters.tutorials.basics.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.enterprise.spring.boot.application.starter.exception.ValidationException;
import de.enterprise.starters.tutorials.basics.persistence.entity.BookEntity;
import de.enterprise.starters.tutorials.basics.persistence.repository.BookRepository;
import lombok.AllArgsConstructor;

/**
 *
 * @author Jonas Keßler
 */
@Service
@AllArgsConstructor
public class BookService {

	private final BookRepository bookRepository;

	public Optional<BookEntity> getBook(Long id) {
		return this.bookRepository.findById(id);
	}

	public BookEntity createBook(BookEntity book) throws ValidationException {
		if (isTitleBlacklisted(book.getTitle())) {
			throw new ValidationException("book.create.title.blacklisted",
					"Book is not allowed to be created. Title is blacklisted - " + book);
		}
		return this.bookRepository.save(book);
	}

	private boolean isTitleBlacklisted(String title) {
		String[] blacklist = { "böse", "blöd" };
		return Arrays.stream(blacklist).anyMatch(b -> title.contains(b));
	}

}

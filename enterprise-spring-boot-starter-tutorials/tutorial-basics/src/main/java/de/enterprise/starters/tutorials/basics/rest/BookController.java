package de.enterprise.starters.tutorials.basics.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.enterprise.spring.boot.application.starter.exception.ResourceNotFoundException;
import de.enterprise.spring.boot.application.starter.exception.ValidationException;
import de.enterprise.starters.tutorials.basics.persistence.entity.BookEntity;
import de.enterprise.starters.tutorials.basics.service.BookService;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;

/**
 *
 * @author Jonas Keßler
 */
@RestController
@AllArgsConstructor
public class BookController {

	private final BookService bookService;

	@ApiResponses({
			@ApiResponse(code = 200, message = "Book found"),
			@ApiResponse(code = 404, message = "Book not found") })
	@GetMapping("/books/{id}")
	public BookEntity getBook(@PathVariable("id") Long id) {
		return this.bookService.getBook(id)
				.orElseThrow(ResourceNotFoundException::new);
	}

	@ApiResponses({
			@ApiResponse(code = 204, message = "Book successful created"),
			@ApiResponse(code = 422, message = "Validation error") })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/books")
	public void postBook(@RequestBody @ApiParam(required = true) BookEntity book) throws ValidationException {
		this.bookService.createBook(book);
	}
}

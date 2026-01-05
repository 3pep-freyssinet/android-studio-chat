package com.google.amara.authenticationretrofit;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
//@Table(name = "REST_DB_ACCESS")
@Table(name = "restdbaccess")
@org.hibernate.annotations.NamedQueries({
    @org.hibernate.annotations.NamedQuery(name = "Book.findAll", query = "SELECT e FROM Book e"),
    @org.hibernate.annotations.NamedQuery(name = "Book.findIsbn", query = "SELECT e FROM Book e WHERE isbn = :isbn"),
	@org.hibernate.annotations.NamedQuery(name = "Book.findAuthor", query = "SELECT e FROM Book e WHERE author = :author"),
	@org.hibernate.annotations.NamedQuery(name = "Book.findId", query = "SELECT e FROM Book e WHERE id = :id"),
	@org.hibernate.annotations.NamedQuery(name = "Book.removeIsbn", query = "DELETE FROM Book e WHERE isbn = :isbn"),
	@org.hibernate.annotations.NamedQuery(name = "Book.removeAuthor", query = "DELETE FROM Book e WHERE author = :author"),
	@org.hibernate.annotations.NamedQuery(name = "Book.removeId", query = "DELETE FROM Book e WHERE e.id = :id")
	

	
})
@XmlRootElement
public class Book {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @Column(length = 40)
  private String isbn;
  
  @Column(length = 40)
  private String author;

  
  //Default constructor
  public Book(){}

  public Book(String author, String isbn){
    this.author = author;
    this.isbn   = isbn;
  }
  
  public int getId() {
    return id;
  }
  
  public void setId(Integer i) {
    id = i;
  }

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(String s) {
    this.isbn = s;
  }
  
  public String getAuthor() {
    return author;
  }

  public void setAuthor(String s) {
    this.author = s;
  }
  
}

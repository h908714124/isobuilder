package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import static javax.tools.Diagnostic.Kind.ERROR;

public final class ValidationException extends Exception {
  private static final long serialVersionUID = 0;
  public final Diagnostic.Kind kind;
  public final Element about;
  ValidationException(Diagnostic.Kind kind, String message, Element about) {
    super(message);
    this.kind = kind;
    this.about = about;
  }
  ValidationException(String message, Element about) {
    this(ERROR, message, about);
  }
}

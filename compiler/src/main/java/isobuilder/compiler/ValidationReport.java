/*
 * Copyright (C) 2014 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package isobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.tools.Diagnostic.Kind.*;

/**
 * A collection of items describing contractual issues with the code as presented to an annotation
 * processor.  A "clean" report (i.e. with no issues) is a report with no {@linkplain Item items}
 * and clean subreports. Callers will typically print the results of the report to a
 * {@link Messager} instance using {@link #printMessagesTo}.
 * <p>
 * <p>A report describes a subject {@link Element}.  Callers may choose to add report items about
 * other elements that are contained within or related to the subject. Since {@link Diagnostic}
 * reporting is expected to be associated with elements that are currently being compiled,
 * {@link #printMessagesTo(Messager)} will only associate messages with non-subject elements if they
 * are contained within the subject. Otherwise, they will be associated with the subject and contain
 * a reference to the item's element in the message string. It is the responsibility of the caller
 * to choose subjects that are part of the compilation.
 *
 * @author Gregory Kick
 * @since 2.0
 */
final class ValidationReport<T extends Element> {

  private final T subject;
  private final ImmutableSet<Item> items;
  private final ImmutableSet<ValidationReport<?>> subreports;

  ValidationReport(T subject, ImmutableSet<Item> items, ImmutableSet<ValidationReport<?>> subreports) {
    this.subject = checkNotNull(subject);
    this.items = checkNotNull(items);
    this.subreports = checkNotNull(subreports);
  }

  T subject() {
    return subject;
  }

  ImmutableSet<Item> items() {
    return items;
  }

  ImmutableSet<ValidationReport<?>> subreports() {
    return subreports;
  }

  boolean isClean() {
    for (Item item : items()) {
      switch (item.kind()) {
        case ERROR:
          return false;
        default:
          break;
      }
    }
    for (ValidationReport<?> subreport : subreports()) {
      if (!subreport.isClean()) {
        return false;
      }
    }
    return true;
  }

  void printMessagesTo(Messager messager) {
    for (Item item : items()) {
      if (isEnclosedIn(subject(), item.element())) {
        if (item.annotation().isPresent()) {
          messager.printMessage(
              item.kind(), item.message(), item.element(), item.annotation().get());
        } else {
          messager.printMessage(item.kind(), item.message(), item.element());
        }
      } else {
        String message = String.format("[%s] %s", elementString(item.element()), item.message());
        if (item.annotation().isPresent()) {
          messager.printMessage(item.kind(), message, subject(), item.annotation().get());
        } else {
          messager.printMessage(item.kind(), message, subject());
        }
      }
    }
    for (ValidationReport<?> subreport : subreports()) {
      subreport.printMessagesTo(messager);
    }
  }

  private static String elementString(Element element) {
    return element.accept(
        new SimpleElementVisitor6<String, Void>() {
          @Override
          protected String defaultAction(Element e, Void p) {
            return e.toString();
          }

          @Override
          public String visitExecutable(ExecutableElement e, Void p) {
            return e.getEnclosingElement().accept(this, null) + '.' + e.toString();
          }
        },
        null);
  }

  private static boolean isEnclosedIn(Element parent, Element child) {
    Element current = child;
    while (current != null) {
      if (current.equals(parent)) {
        return true;
      }
      current = current.getEnclosingElement();
    }
    return false;
  }

  static final class Item {
    private final String message;
    private final Kind kind;
    private final Element element;
    private final Optional<AnnotationMirror> annotation;

    Item(String message, Kind kind, Element element, Optional<AnnotationMirror> annotation) {
      this.message = checkNotNull(message);
      this.kind = checkNotNull(kind);
      this.element = checkNotNull(element);
      this.annotation = checkNotNull(annotation);
    }

    String message() {
      return message;
    }

    Kind kind() {
      return kind;
    }

    Element element() {
      return element;
    }

    Optional<AnnotationMirror> annotation() {
      return annotation;
    }
  }

  static <T extends Element> Builder<T> about(T subject) {
    return new Builder<T>(subject);
  }

  static final class Builder<T extends Element> {
    private final T subject;
    private final ImmutableSet.Builder<Item> items = ImmutableSet.builder();
    private final ImmutableSet.Builder<ValidationReport<?>> subreports = ImmutableSet.builder();

    private Builder(T subject) {
      this.subject = subject;
    }

    T getSubject() {
      return subject;
    }

    Builder<T> addItems(Iterable<Item> newItems) {
      items.addAll(newItems);
      return this;
    }

    Builder<T> addError(String message) {
      return addItem(message, ERROR, subject, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addError(String message, Element element) {
      return addItem(message, ERROR, element, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addError(String message, Element element, AnnotationMirror annotation) {
      return addItem(message, ERROR, element, Optional.of(annotation));
    }

    Builder<T> addWarning(String message) {
      return addItem(message, WARNING, subject, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addWarning(String message, Element element) {
      return addItem(message, WARNING, element, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addWarning(String message, Element element, AnnotationMirror annotation) {
      return addItem(message, WARNING, element, Optional.of(annotation));
    }

    Builder<T> addNote(String message) {
      return addItem(message, NOTE, subject, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addNote(String message, Element element) {
      return addItem(message, NOTE, element, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addNote(String message, Element element, AnnotationMirror annotation) {
      return addItem(message, NOTE, element, Optional.of(annotation));
    }

    Builder<T> addItem(String message, Kind kind, Element element) {
      return addItem(message, kind, element, Optional.<AnnotationMirror>absent());
    }

    Builder<T> addItem(String message, Kind kind, Element element, AnnotationMirror annotation) {
      return addItem(message, kind, element, Optional.of(annotation));
    }

    private Builder<T> addItem(String message, Kind kind, Element element,
                               Optional<AnnotationMirror> annotation) {
      items.add(new Item(message, kind, element, annotation));
      return this;
    }

    Builder<T> addSubreport(ValidationReport<?> subreport) {
      subreports.add(subreport);
      return this;
    }

    ValidationReport<T> build() {
      return new ValidationReport<T>(subject, items.build(), subreports.build());
    }
  }
}

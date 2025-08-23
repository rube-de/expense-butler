# Development Workflow

## Overview

This document defines the step-by-step workflow for implementing features in the Expense Tracker project. For detailed practices, see the referenced steering documents.

## Task Execution Workflow

### 1. Task Analysis
- [ ] Read task requirements from `tasks.md`
- [ ] Review related requirements in `requirements.md`
- [ ] Check design specifications in `design.md`
- [ ] Identify dependencies and prerequisites
- [ ] **CRITICAL**: Review [Android Best Practices](android-best-practices.md) for applicable patterns

### 1.5 Pre-Implementation Review
- [ ] **Review Android Best Practices**: Consult [android-best-practices.md](android-best-practices.md) for applicable patterns
- [ ] **Plan Architecture**: Database migrations, state management, validation, performance, i18n, navigation

### 2. Test-First Development (TDD)
- [ ] Follow Red-Green-Refactor cycle for all implementations
- [ ] **Details**: See [tdd.md](tdd.md) for comprehensive TDD practices

### 3. Implementation Guidelines
- [ ] **Architecture**: Follow MVVM, Repository patterns from [structure.md](structure.md)
- [ ] **Code Quality**: Apply principles from [coding-principles.md](coding-principles.md)
- [ ] **Android Practices**: Implement patterns from [android-best-practices.md](android-best-practices.md)
- [ ] **Technology**: Use stack from [tech.md](tech.md)

### 4. Quality Assurance
- [ ] **Testing**: Run full test suite - commands in [tech.md](tech.md)
- [ ] **Coverage**: Meet requirements from [tdd.md](tdd.md)
- [ ] **Standards**: Verify compliance with [android-best-practices.md](android-best-practices.md)

### 5. Task Completion
- [ ] Update task status to completed
- [ ] Verify all acceptance criteria met
- [ ] Document any architectural decisions
- [ ] Prepare for next task

## Workflow Integration

### Development Process
1. **TDD Cycle**: Follow detailed process in [tdd.md](tdd.md)
2. **Quality Standards**: Apply principles from [coding-principles.md](coding-principles.md)
3. **Android Practices**: Implement patterns from [android-best-practices.md](android-best-practices.md)

## Quality Review

### Review Checklists
- [ ] **TDD Compliance**: See checklist in [tdd.md](tdd.md)
- [ ] **Code Quality**: Apply standards from [coding-principles.md](coding-principles.md)
- [ ] **Android Standards**: Follow checklist in [android-best-practices.md](android-best-practices.md)

## Testing and CI/CD

### Testing
- **Strategy**: See comprehensive testing patterns in [tdd.md](tdd.md)
- **Commands**: Build and test commands in [tech.md](tech.md)
- **Coverage**: Requirements defined in [tdd.md](tdd.md)

## Documentation References

- **[android-best-practices.md](android-best-practices.md)**: Critical patterns and architectural decisions
- **[tdd.md](tdd.md)**: Comprehensive TDD practices and testing strategies
- **[coding-principles.md](coding-principles.md)**: Code quality principles (KISS, High Cohesion/Low Coupling, POLA)
- **[tech.md](tech.md)**: Technology stack, tools, and build commands
- **[structure.md](structure.md)**: Project organization and architecture patterns
- **[product.md](product.md)**: Business context and requirements

## Success Criteria

### Task Completion Requirements
- [ ] All tests pass (see [tdd.md](tdd.md) for coverage requirements)
- [ ] Code review checklist from [android-best-practices.md](android-best-practices.md) completed
- [ ] Build succeeds with no lint warnings
- [ ] All acceptance criteria met

**This workflow ensures consistent, production-ready development by leveraging the established quality standards documented in the steering guides.**
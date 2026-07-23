# Frontend review checklist

- [ ] i18n keys added to all four locale files (en, es, fr, pt)
- [ ] New components generated with `ng generate`, not created manually
- [ ] Imports use `@core`/`@features`/`@shared` aliases, not relative paths
- [ ] Signals used for state (no `BehaviorSubject`, no NgRx)
- [ ] Toast strings go through `TranslocoService.translate()` before `ToastService`
- [ ] `pointer-events` on toast container not changed
- [ ] New component listed in `docs/architecture/frontend.md` folder tree
- [ ] New route added to the Routing Pattern section in `docs/architecture/frontend.md`
- [ ] `docs/CHANGELOG.md` updated if this closes a feature issue

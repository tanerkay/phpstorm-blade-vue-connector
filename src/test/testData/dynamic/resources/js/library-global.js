import * as Lib from 'bootstrap-vue-next'

export function registerLibrary(app) {
    for (const [name, exported] of Object.entries(Lib)) {
        if (/^B[A-Z]/.test(name)) {
            app.component(name, exported)
        }
    }
}

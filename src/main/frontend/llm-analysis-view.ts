import { html, LitElement } from 'lit';
import { customElement, state } from 'lit/decorators.js';

type MajorIssue = { title: string; evidence: string[]; severity: string };
type Recommendation = { title: string; rationale: string; impact: string };
type Patch = { path: string; patch: string; description?: string };
type LlmResponse = { summary: string; majorIssues: MajorIssue[]; recommendations: Recommendation[]; patches: Patch[] };
type ApplyResult = { success: boolean; branchName: string; filesTouched: string[]; log: string; message: string };

@customElement('llm-analysis-view')
export class LlmAnalysisView extends LitElement {
  @state() private loading = false;
  @state() private error: string | null = null;
  @state() private data: LlmResponse | null = null;
  @state() private applyResult: ApplyResult | null = null;
  @state() private applying = false;

  render() {
    return html`
      <div style="display:flex; flex-direction:column; gap:12px;">
        <div style="display:flex; gap:8px;">
          <button @click=${this.fetchData} ?disabled=${this.loading}>Analyze with LLM</button>
          ${this.loading ? html`<span>Loading...</span>` : ''}
          ${this.error ? html`<span style="color:red;">${this.error}</span>` : ''}
        </div>

        ${this.data ? this.renderContent(this.data) : html`<div>No analysis yet.</div>`}
        ${this.renderApplySection()}
      </div>
    `;
  }

  private renderContent(data: LlmResponse) {
    return html`
      <section>
        <h3>Overall system health</h3>
        <p>${data.summary}</p>
      </section>
      <section>
        <h3>Major issues</h3>
        <ul>
          ${data.majorIssues?.map(
            (issue) => html`<li>
              <strong>${issue.title}</strong> [${issue.severity}]<br />
              Evidence:
              <ul>
                ${issue.evidence?.map((e) => html`<li>${e}</li>`)}
              </ul>
            </li>`
          )}
        </ul>
      </section>
      <section>
        <h3>Recommended improvements</h3>
        <ol>
          ${data.recommendations?.map(
            (rec) => html`<li>
              <strong>${rec.title}</strong> [${rec.impact}]<br />
              ${rec.rationale}
            </li>`
          )}
        </ol>
      </section>
      <section>
        <h3>Suggested patches (existing files only)</h3>
        <ul>
          ${data.patches?.map(
            (p) => html`<li>
              <strong>${p.path}</strong>
              ${p.description ? html`<div>${p.description}</div>` : ''}
              <details>
                <summary>View diff</summary>
                <pre style="white-space:pre-wrap; background:#f7f7f7; padding:8px; border:1px solid #ddd;">${p.patch}</pre>
              </details>
            </li>`
          )}
        </ul>
      </section>
    `;
  }

  private renderApplySection() {
    return html`
      <section style="margin-top:12px; padding:8px; border:1px solid #ddd; border-radius:4px;">
        <div style="display:flex; gap:8px; align-items:center;">
          <button @click=${this.applyPatches} ?disabled=${this.applying || !this.data}>
            ${this.applying ? 'Applying…' : 'Apply patches to temp branch'}
          </button>
          ${!this.data ? html`<span>Run analysis first.</span>` : ''}
          ${this.error ? html`<span style="color:red;">${this.error}</span>` : ''}
        </div>
        ${this.applyResult ? this.renderApplyResult(this.applyResult) : ''}
      </section>
    `;
  }

  private renderApplyResult(result: ApplyResult) {
    return html`
      <div style="margin-top:8px;">
        <div>Status: <strong style="color:${result.success ? 'green' : 'red'};">${result.success ? 'Success' : 'Failure'}</strong></div>
        <div>Branch: ${result.branchName}</div>
        <div>Files touched:</div>
        <ul>
          ${result.filesTouched?.map((f) => html`<li>${f}</li>`)}
        </ul>
        <details>
          <summary>Logs</summary>
          <pre style="white-space:pre-wrap; background:#f7f7f7; padding:8px; border:1px solid #ddd;">${result.log}</pre>
        </details>
        <div>${result.message}</div>
      </div>
    `;
  }

  private async fetchData() {
    this.loading = true;
    this.error = null;
    try {
      const res = await fetch('/api/llm/analysis');
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.data = await res.json();
      this.applyResult = null;
    } catch (err: any) {
      this.error = err?.message ?? 'Request failed';
    } finally {
      this.loading = false;
    }
  }

  private async applyPatches() {
    if (!this.data) return;
    this.applying = true;
    this.error = null;
    try {
      const res = await fetch('/api/llm/apply', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.data),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.applyResult = await res.json();
    } catch (err: any) {
      this.error = err?.message ?? 'Apply failed';
    } finally {
      this.applying = false;
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'llm-analysis-view': LlmAnalysisView;
  }
}

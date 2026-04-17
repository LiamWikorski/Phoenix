import { html, LitElement } from 'lit';
import { customElement, state } from 'lit/decorators.js';

type MajorIssue = { title: string; evidence: string[]; severity: string };
type Recommendation = { title: string; rationale: string; impact: string };
type LlmResponse = { summary: string; majorIssues: MajorIssue[]; recommendations: Recommendation[] };

@customElement('llm-analysis-view')
export class LlmAnalysisView extends LitElement {
  @state() private loading = false;
  @state() private error: string | null = null;
  @state() private data: LlmResponse | null = null;

  render() {
    return html`
      <div style="display:flex; flex-direction:column; gap:12px;">
        <div style="display:flex; gap:8px;">
          <button @click=${this.fetchData} ?disabled=${this.loading}>Analyze with LLM</button>
          ${this.loading ? html`<span>Loading...</span>` : ''}
          ${this.error ? html`<span style="color:red;">${this.error}</span>` : ''}
        </div>

        ${this.data ? this.renderContent(this.data) : html`<div>No analysis yet.</div>`}
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
    `;
  }

  private async fetchData() {
    this.loading = true;
    this.error = null;
    try {
      const res = await fetch('/api/llm/analysis');
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.data = await res.json();
    } catch (err: any) {
      this.error = err?.message ?? 'Request failed';
    } finally {
      this.loading = false;
    }
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'llm-analysis-view': LlmAnalysisView;
  }
}

import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import Chart from 'chart.js/auto';

@customElement('error-volume-chart')
export class ErrorVolumeChart extends LitElement {
  static styles = css`
    :host {
      display: block;
    }

    canvas {
      width: 100%;
      height: 100%;
    }
  `;

  private chart?: Chart;
  private pendingLabels: string[] = [];
  private pendingValues: number[] = [];
  private chartInitialised = false;

  render() {
    return html`<canvas></canvas>`;
  }

  firstUpdated() {
    const canvas = this.renderRoot.querySelector('canvas');
    if (!canvas) {
      return;
    }

    const context = canvas.getContext('2d');
    if (!context) {
      return;
    }

    this.chart = new Chart(context, {
      type: 'bar',
      data: {
        labels: this.pendingLabels,
        datasets: [
          {
            label: 'Errors',
            data: this.pendingValues,
            borderWidth: 1,
            backgroundColor: '#2563eb'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        }
      }
    });

    this.chartInitialised = true;
  }

  public setChartData(labels: string[], values: number[]) {
    this.pendingLabels = labels;
    this.pendingValues = values;

    if (!this.chartInitialised || !this.chart) {
      return;
    }

    this.chart.data.labels = labels;
    if (this.chart.data.datasets.length === 0) {
      this.chart.data.datasets.push({
        label: 'Errors',
        data: values,
        borderWidth: 1,
        backgroundColor: '#2563eb'
      });
    } else {
      this.chart.data.datasets[0].data = values;
    }
    this.chart.update();
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'error-volume-chart': ErrorVolumeChart;
  }
}


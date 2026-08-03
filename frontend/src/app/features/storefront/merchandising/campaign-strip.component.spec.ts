import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CampaignStripComponent } from './campaign-strip.component';
import { MerchandisingService } from '../../../core/services/merchandising.service';
import { CampaignResponse } from '../../../core/models/campaign.model';

describe('CampaignStripComponent', () => {
  let fixture: ComponentFixture<CampaignStripComponent>;
  let component: CampaignStripComponent;
  let merch: jasmine.SpyObj<MerchandisingService>;

  const campaign: CampaignResponse = {
    id: 1, name: 'Summer Sale', status: 'PUBLISHED', active: true, couponCode: 'SUMMER25'
  };

  beforeEach(async () => {
    merch = jasmine.createSpyObj<MerchandisingService>('MerchandisingService', [
      'effectiveCampaigns', 'recordEvent'
    ]);
    merch.effectiveCampaigns.and.returnValue(
      of({ success: true, message: 'ok', data: [campaign], timestamp: new Date().toISOString() })
    );

    await TestBed.configureTestingModule({
      imports: [CampaignStripComponent],
      providers: [{ provide: MerchandisingService, useValue: merch }]
    }).compileComponents();

    fixture = TestBed.createComponent(CampaignStripComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the effective campaigns on init', () => {
    expect(merch.effectiveCampaigns).toHaveBeenCalled();
    expect(component.campaigns.length).toBe(1);
    expect(component.campaigns[0].couponCode).toBe('SUMMER25');
  });

  it('records a CAMPAIGN impression for a campaign', () => {
    component.onImpression(campaign);
    expect(merch.recordEvent).toHaveBeenCalledWith('IMPRESSION', 'CAMPAIGN', 1);
  });

  it('records a CAMPAIGN click', () => {
    component.onClick(campaign);
    expect(merch.recordEvent).toHaveBeenCalledWith('CLICK', 'CAMPAIGN', 1);
  });
});
